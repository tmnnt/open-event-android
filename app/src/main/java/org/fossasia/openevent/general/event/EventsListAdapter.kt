package org.fossasia.openevent.general.event

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagedListAdapter
import kotlinx.android.synthetic.main.item_card_events.view.favoriteFab
import kotlinx.android.synthetic.main.item_card_events.view.shareFab
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.common.EventClickListener
import org.fossasia.openevent.general.common.EventsDiffCallback
import org.fossasia.openevent.general.common.FavoriteFabClickListener

/**
 * The RecyclerView adapter class for displaying lists of Events.
 *
 * @param eventLayout The layout type to be used in this adapter.
 * @param diffCallback The DiffUtil.ItemCallback implementation to be used with this adapter
 * @property onEventClick The callback to be invoked when an event is clicked
 * @property onFavFabClick The callback to be invoked when the favorite FAB is clicked
 * @property onShareFabClick The callback to be invoked when the share FAB is clicked
 */
class EventsListAdapter(
    diffCallback: EventsDiffCallback
) : PagedListAdapter<Event, EventViewHolder>(diffCallback) {
    var onEventClick: EventClickListener? = null
    var onFavFabClick: FavoriteFabClickListener? = null
    var onHashtagClick: EventHashTagClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val eventView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_card_events, parent, false)
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
            hashTagClickListAdapter = onHashtagClick
        }
    }

    /**
     * The function to call when the adapter has to be cleared of items
     */
    fun clear() {
        this.currentList?.clear()
    }
}

interface EventHashTagClickListener {
    fun onClick(hashTagValue: String)
}
