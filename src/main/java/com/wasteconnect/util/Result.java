package com.wasteconnect.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CO4: Generic functional monadic Result container encapsulating success values or operational errors.
 *
 * @param <T> Payload type
 */
public final class Result<T> {
    private final T data;
    private final String errorMessage;
    private final boolean success;

    private Result(T data, String errorMessage, boolean success) {
        this.data = data;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }

    public static <T> Result<T> failure(String errorMessage) {
        return new Result<>(null, Objects.requireNonNull(errorMessage), false);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getData() {
        if (!success) {
            throw new IllegalStateException("Attempted to access data on a failed Result: " + errorMessage);
        }
        return data;
    }

    public Optional<T> toOptional() {
        return Optional.ofNullable(data);
    }

    public String getErrorMessage() {
        return errorMessage != null ? errorMessage : "";
    }

    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        if (!success) {
            return Result.failure(errorMessage);
        }
        try {
            return Result.success(mapper.apply(data));
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result<T> onSuccess(Consumer<? super T> action) {
        if (success) {
            action.accept(data);
        }
        return this;
    }

    public Result<T> onFailure(Consumer<String> action) {
        if (!success) {
            action.accept(errorMessage);
        }
        return this;
    }

    @Override
    public String toString() {
        return success ? "Success[" + data + "]" : "Failure[" + errorMessage + "]";
    }
}
