package com.lil.safetagv2rppsservice.exception;

public class RppsExceptions {

    // On ne veut pas que cette classe soit instanciée
    private RppsExceptions() {}

    public static class BaseException extends RuntimeException {
        public BaseException(String message) { super(message); }
        public BaseException(String message, Throwable cause) { super(message, cause); }
    }

    public static class CommunicationException extends BaseException {
        public CommunicationException(String message, Throwable cause) { super(message, cause); }
    }

    public static class NotFoundException extends BaseException {
        public NotFoundException(String message) { super(message); }
    }
}