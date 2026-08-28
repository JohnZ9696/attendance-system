import React, { useState } from 'react';
import { Send, CheckCircle2, AlertCircle, Info } from 'lucide-react';
import { apiClient } from '../api/client';
import './Notify.css';

export default function Notify() {
  const [message, setMessage] = useState('');
  const [status, setStatus] = useState('idle'); // idle, sending, success, error
  const [errorMessage, setErrorMessage] = useState('');

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!message.trim()) return;

    setStatus('sending');
    setErrorMessage('');

    try {
      await apiClient.sendNotification(message.trim());
      setStatus('success');
      setMessage('');
      
      // Reset status after a few seconds
      setTimeout(() => {
        setStatus('idle');
      }, 3000);
    } catch (error) {
      setStatus('error');
      setErrorMessage(error.message || 'Lỗi khi gửi thông báo. Vui lòng thử lại.');
    }
  };

  return (
    <div className="notify-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Gửi thông báo OLED</h1>
          <p className="page-subtitle">Nhập nội dung để gửi hiển thị lên màn hình OLED cho học sinh/sinh viên</p>
        </div>
      </div>

      <div className="notify-content card">
        <form onSubmit={handleSendMessage} className="notify-form">
          <div className="form-group">
            <label htmlFor="notification-message">Nội dung thông báo</label>
            <textarea
              id="notification-message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Nhập nội dung hiển thị lên màn hình OLED..."
              rows={4}
              maxLength={128} // assuming some limit for OLED
              disabled={status === 'sending'}
              className="notify-textarea"
            />
            <div className="char-count">
              {message.length} / 128 ký tự
            </div>
          </div>

          <div className="form-actions">
            <button 
              type="submit" 
              className={`btn btn-primary btn-send ${status === 'sending' ? 'loading' : ''}`}
              disabled={!message.trim() || status === 'sending'}
            >
              <Send size={18} />
              {status === 'sending' ? 'Đang gửi...' : 'Gửi thông báo'}
            </button>
          </div>
        </form>

        {status === 'success' && (
          <div className="alert alert-success">
            <CheckCircle2 size={20} />
            <div>
              <strong>Thành công!</strong>
              <p>Thông báo đã được gửi đến thiết bị ESP32.</p>
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="alert alert-error">
            <AlertCircle size={20} />
            <div>
              <strong>Lỗi!</strong>
              <p>{errorMessage}</p>
            </div>
          </div>
        )}
        
        <div className="alert alert-info" style={{ marginTop: '24px' }}>
          <Info size={20} />
          <div>
            <strong>Lưu ý:</strong>
            <p>Màn hình OLED có kích thước nhỏ, hãy giữ thông báo ngắn gọn và súc tích để hiển thị rõ ràng nhất. Các ký tự tiếng Việt có dấu có thể hiển thị không chính xác nếu thiết bị không hỗ trợ font tương ứng.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
