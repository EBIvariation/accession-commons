/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ampt2d.commons.accession.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.MissingUnsavedAccessionsException;
import uk.ac.ebi.ampt2d.commons.accession.rest.controllers.BasicRestController;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.ErrorMessage;
import uk.ac.ebi.ampt2d.commons.accession.rest.validation.CollectionValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * Spring {@link RestControllerAdvice} bean to handle exception from the application at rest level and return
 * a specific response with an error message.
 */
@RestControllerAdvice(assignableTypes = BasicRestController.class)
public class BasicRestControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(BasicRestControllerAdvice.class);

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private CollectionValidator collectionValidator;

    @ExceptionHandler(value = {AccessionIsNotPendingException.class, AccessionCouldNotBeGeneratedException.class,
            MissingUnsavedAccessionsException.class})
    public ResponseEntity<ErrorMessage> handleInternalServerErrors(Exception ex) {
        logger.error(ex.getMessage(), ex);
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex, ex.getMessage());
    }

    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorMessage> handleHttpMediaNotSupported(HttpMediaTypeNotSupportedException ex) {
        return buildResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex, ex.getMessage());
    }

    @ExceptionHandler(value = ValidationException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(ValidationException ex) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex, ex.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex, ex.getBindingResult().getAllErrors().
                stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining("\n")));
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex, "Please provide accepted values");
    }

    @ExceptionHandler(value = {AccessionDoesNotExistException.class})
    public ResponseEntity<ErrorMessage> handleNotFoundErrors(Exception ex) {
        logger.error(ex.getMessage(), ex);
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex, ex.getMessage());
    }

    @ExceptionHandler(value = AccessionMergedException.class)
    public ResponseEntity<ErrorMessage> handleMergeExceptions(AccessionMergedException ex) {
        logger.error(ex.getMessage(), ex);
        if (httpServletRequest.getMethod().equals(HttpMethod.GET.name())) {
            String originalRequestUrl = httpServletRequest.getRequestURL().toString();
            String newUrl = replaceAccession(originalRequestUrl, ex.getOriginAccessionId(),
                                             ex.getDestinationAccessionId());

            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                                 .location(URI.create(newUrl))
                                 .body(new ErrorMessage(HttpStatus.MOVED_PERMANENTLY, ex, ex.getMessage()));
        }
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex, ex.getMessage());
    }

    private String replaceAccession(String originalRequestUrl, String originAccession, String destinationAccession) {
        int lastAccessionOccurrenceStart = originalRequestUrl.lastIndexOf(originAccession);
        int lastAccessionOccurrenceEnd = lastAccessionOccurrenceStart + originAccession.length();
        return originalRequestUrl.substring(0, lastAccessionOccurrenceStart)
               + destinationAccession
               + originalRequestUrl.substring(lastAccessionOccurrenceEnd);
    }

    @ExceptionHandler(value = {HashAlreadyExistsException.class})
    public ResponseEntity<ErrorMessage> handleConflictErrors(Exception ex) {
        logger.error(ex.getMessage(), ex);
        return buildResponseEntity(HttpStatus.CONFLICT, ex, ex.getMessage());
    }

    @ExceptionHandler(value = {AccessionDeprecatedException.class})
    public ResponseEntity<ErrorMessage> handleDeprecationErrors(Exception ex) {
        logger.error(ex.getMessage(), ex);
        return buildResponseEntity(HttpStatus.GONE, ex, "This accession has been deprecated");
    }

    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<ErrorMessage> handleIllegalArgumentErrors(Exception ex) {
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex, ex.getMessage());
    }

    private ResponseEntity<ErrorMessage> buildResponseEntity(HttpStatus status, Exception ex, String message) {
        return new ResponseEntity<>(new ErrorMessage(status, ex, message), status);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        if (binder.getTarget() != null && collectionValidator.supports(binder.getTarget().getClass()))
            binder.addValidators(collectionValidator);
    }

}
