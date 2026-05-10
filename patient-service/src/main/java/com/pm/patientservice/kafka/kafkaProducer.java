package com.pm.patientservice.kafka;

import com.pm.patientservice.exception.EventPublishingException;
import com.pm.patientservice.model.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

import java.util.concurrent.ExecutionException;

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
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while publishing PatientCreated event for patientId={}",
              patient.getId(), e);
      throw new EventPublishingException(
              "Interrupted while publishing PatientCreated event", e);
    } catch (ExecutionException e) {
      log.error("Failed to publish PatientCreated event for patientId={}",
              patient.getId(), e);
      throw new EventPublishingException(
              "Failed to publish PatientCreated event", e);
    }
  }
}
