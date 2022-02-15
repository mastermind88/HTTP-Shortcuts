package ch.rmy.android.http_shortcuts.activities.main

import com.victorrendina.rxqueue2.QueueSubject
import io.reactivex.Observable

class ChildViewModelEventBridge {

    fun submit(event: ChildViewModelEvent) {
        eventSubject.onNext(event)
    }

    val events: Observable<ChildViewModelEvent> =
        eventSubject

    companion object {
        private val eventSubject = QueueSubject.create<ChildViewModelEvent>()
    }
}
