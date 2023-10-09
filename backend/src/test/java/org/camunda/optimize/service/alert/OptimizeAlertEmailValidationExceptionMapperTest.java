/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.alert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.camunda.optimize.dto.optimize.rest.AlertEmailValidationResponseDto;
import org.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import org.camunda.optimize.rest.providers.OptimizeAlertEmailValidationExceptionMapper;
import org.camunda.optimize.service.exceptions.OptimizeAlertEmailValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OptimizeAlertEmailValidationExceptionMapperTest {

  @Test
  public void exceptionContainsListOfInvalidEmails() {
    // given
    final Set<String> invalidEmails = Set.of("invalid@email.com", "another@bademail.com");
    final OptimizeAlertEmailValidationException emailValidationException =
      new OptimizeAlertEmailValidationException(invalidEmails);
    final OptimizeAlertEmailValidationExceptionMapper underTest =
      new OptimizeAlertEmailValidationExceptionMapper();

    // when
    final Response response = underTest.toResponse(emailValidationException);
    Map<String, Object> mappedResponse = new ObjectMapper()
      .convertValue(response.getEntity(), new TypeReference<>() {
      });

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(mappedResponse.get(ErrorResponseDto.Fields.errorCode)).asString()
      .isEqualTo(OptimizeAlertEmailValidationException.ERROR_CODE);
    assertThat(mappedResponse.get(ErrorResponseDto.Fields.errorMessage)).asString()
      .isEqualTo(OptimizeAlertEmailValidationException.ERROR_MESSAGE + invalidEmails);
    assertThat(mappedResponse.get(ErrorResponseDto.Fields.detailedMessage)).asString()
      .isEqualTo(OptimizeAlertEmailValidationException.ERROR_MESSAGE + invalidEmails);
    assertThat(mappedResponse.get(AlertEmailValidationResponseDto.Fields.invalidAlertEmails)).asString()
      .isEqualTo(String.join(", ", invalidEmails));
  }

}
