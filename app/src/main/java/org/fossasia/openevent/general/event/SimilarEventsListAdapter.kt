package org.fossasia.openevent.general.event

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import kotlinx.android.synthetic.main.item_card_similar_events.view.favoriteFab
import kotlinx.android.synthetic.main.item_card_similar_events.view.shareFab
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.common.EventClickListener
import org.fossasia.openevent.general.common.EventsDiffCallback
import org.fossasia.openevent.general.common.FavoriteFabClickListener

class SimilarEventsListAdapter(
    diffCallback: EventsDiffCallback
) : ListAdapter<Event, EventViewHolder>(diffCallback) {
    var onEventClick: EventClickListener? = null
    var onFavFabClick: FavoriteFabClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val eventView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_similar_events, parent, false)

        eventView.shareFab.scaleType = ImageView.ScaleType.CENTER
        eventView.favoriteFab.scaleType = ImageView.ScaleType.CENTER
        return EventViewHolder(eventView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.apply {
            if (event != null) {
                bind(event, EVENT_DATE_FORMAT)
            }
            eventClickListener = onEventClick
            favFabClickListener = onFavFabClick
        }
    }

    /**
     * The function to call when the adapter has to be cleared of items
     */
    fun clear() {
        this.submitList(emptyList())
    }
}
