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
package uk.ac.ebi.ampt2d.commons.accession.rest.validation;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import java.util.Collection;
import java.util.Set;

/**
 * Validator class for Collection of DTOs
 */
public class CollectionValidator implements Validator {

    private LocalValidatorFactoryBean validator;

    public CollectionValidator(LocalValidatorFactoryBean validator) {
        this.validator = validator;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return Collection.class.isAssignableFrom(aClass);
    }

    public void validate(Object target, Errors errors) {
        final StringBuilder message = new StringBuilder();
        Object[] objects = ((Collection) target).toArray();
        for (int indexOfObjects = 0; indexOfObjects < objects.length; indexOfObjects++) {
            Set<ConstraintViolation<Object>> constraintViolations = validator.validate(objects[indexOfObjects], new Class[0]);
            if (constraintViolations != null && constraintViolations.size() > 0) {
                message.append(errors.getObjectName() + "[" + indexOfObjects + "] : ");
                constraintViolations.stream().forEach(constraintViolation -> {
                    message.append(constraintViolation.getMessage() + "\n");
                });
            }
        }
        if (message.length() > 0) {
            throw new ValidationException(message.toString());
        }
    }
}