import asyncio
import logging


logger = logging.getLogger("uvicorn.error")

DISCOVERY_REQUEST = b"ATTENDANCE_AI_DISCOVER_V1"
DISCOVERY_RESPONSE_PREFIX = "ATTENDANCE_AI_SERVER_V1:"


class AiDiscoveryProtocol(asyncio.DatagramProtocol):
    def __init__(self, server_port: int) -> None:
        self.server_port = server_port
        self.transport: asyncio.DatagramTransport | None = None

    def connection_made(self, transport: asyncio.BaseTransport) -> None:
        self.transport = transport  # type: ignore[assignment]

    def datagram_received(self, data: bytes, address: tuple[str, int]) -> None:
        if data != DISCOVERY_REQUEST or self.transport is None:
            return

        response = f"{DISCOVERY_RESPONSE_PREFIX}{self.server_port}".encode("ascii")
        self.transport.sendto(response, address)


async def start_udp_discovery(
    discovery_port: int,
    server_port: int,
) -> asyncio.DatagramTransport:
    loop = asyncio.get_running_loop()
    transport, _ = await loop.create_datagram_endpoint(
        lambda: AiDiscoveryProtocol(server_port),
        local_addr=("0.0.0.0", discovery_port),
        allow_broadcast=True,
    )
    logger.info("AI UDP discovery listening on port %s", discovery_port)
    return transport
