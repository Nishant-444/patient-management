package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class kafkaProducer {
  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public void sendEvent(Patient patient) {
    PatientEvent event = PatientEvent.newBuilder()
            .setPatientId(patient.getId().toString())
            .setName(patient.getName())
            .setEmail(patient.getEmail())
            .setEventType("PATIENT_CREATED")
            .build();
    try {
      log.info("Patient event {}", event);
      kafkaTemplate.send("patient", event.toByteArray()).get();

    } catch (Exception e) {
      log.error("Error sending PatientCreated event: ", e);
    }
  }
}
