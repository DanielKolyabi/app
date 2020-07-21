package ru.relabs.kurjer.utils

/**
 * Created by Daniil Kurchanov on 05.11.2019.
 */


sealed class Either<out L, out R> {

    companion object {
        inline fun <R> of(action: () -> R): Either<Exception, R> {
            return try {
                Right(action())
            } catch (ex: Exception) {
                Left(ex)
            }
        }
    }
}

data class Right<out R>(val value: R) : Either<Nothing, R>()
data class Left<out L>(val value: L) : Either<L, Nothing>()

fun <L, R, U> Either<L, R>.fmap(transform: (R) -> U): Either<L, U> {
    return when (this) {
        is Right -> Right(transform(this.value))
        is Left -> this
    }
}

inline infix fun <L, R, U> Either<L, R>.bind(transform: (R) -> Either<L, U>): Either<L, U> {
    return when (this) {
        is Right -> transform(this.value)
        is Left -> this
    }
}