package com.iot.attendance.service;

import com.iot.attendance.entity.Student;
import com.iot.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    
    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll().stream()
                .filter(Student::getIsActive)
                .collect(Collectors.toList());
    }

    public Optional<Student> getStudentById(UUID id) {
        return studentRepository.findById(id).filter(Student::getIsActive);
    }

    public Optional<Student> getStudentByUid(String uid) {
        if (uid == null) return Optional.empty();
        String normalizedUid = uid.toUpperCase().replace(" ", "").replace(":", "");
        return studentRepository.findByUidAndIsActiveTrue(normalizedUid);
    }

    @Transactional
    public Student createStudent(Student student) {
        if (studentRepository.findByMssvAndIsActiveTrue(student.getMssv()).isPresent()) {
            throw new IllegalArgumentException("Student with MSSV " + student.getMssv() + " already exists.");
        }
        if (student.getUid() != null) {
            String normalizedUid = student.getUid().toUpperCase().replace(" ", "").replace(":", "");
            student.setUid(normalizedUid);
        }
        student.setIsActive(true);
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(UUID id, Student updates) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        
        if (updates.getFullName() != null) {
            student.setFullName(updates.getFullName());
        }
        if (updates.getUid() != null) {
            String normalizedUid = updates.getUid().toUpperCase().replace(" ", "").replace(":", "");
            student.setUid(normalizedUid);
        }
        if (updates.getIsActive() != null) {
            student.setIsActive(updates.getIsActive());
        }
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(UUID id) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student != null) {
            student.setIsActive(false);
            studentRepository.save(student);
        }
    }

    @Transactional
    public Student updateFaceEmbedding(UUID id, String embedding, String model, int dimension) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        student.setFaceEmbedding(embedding);
        student.setFaceModel(model);
        student.setEmbeddingDimension(dimension);
        return studentRepository.save(student);
    }
}
