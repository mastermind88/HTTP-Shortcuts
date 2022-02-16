package ch.rmy.android.framework.viewmodel

import ch.rmy.android.framework.utils.Optional
import com.victorrendina.rxqueue2.QueueSubject
import io.reactivex.Observable

class EventBridge<T : Any> {

    fun submit(event: T) {
        eventSubject.onNext(event)
    }

    val events: Observable<T> =
        eventSubject
            .map { Optional(it as? T) }
            .filter { it.value != null }
            .map { it.value!! }

    companion object {
        private val eventSubject = QueueSubject.create<Any>()
    }
}
