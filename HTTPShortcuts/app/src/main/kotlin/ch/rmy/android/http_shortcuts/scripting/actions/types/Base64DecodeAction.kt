package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.util.Base64
import ch.rmy.android.http_shortcuts.exceptions.ActionException
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import io.reactivex.Single

class Base64DecodeAction(private val encoded: String) : BaseAction() {

    override fun executeForValue(executionContext: ExecutionContext): Single<Any> =
        Single.fromCallable {
            try {
                String(Base64.decode(encoded, Base64.DEFAULT))
            } catch (e: IllegalArgumentException) {
                throw ActionException {
                    "Invalid Base64: $encoded" // TODO: Localize
                }
            }
        }
}
