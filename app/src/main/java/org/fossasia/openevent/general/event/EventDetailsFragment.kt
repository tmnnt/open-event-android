package org.fossasia.openevent.general.event

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.content_event.similarEventsContainer
import kotlinx.android.synthetic.main.content_event.view.eventDateDetailsFirst
import kotlinx.android.synthetic.main.content_event.view.eventDateDetailsSecond
import kotlinx.android.synthetic.main.content_event.view.eventDescription
import kotlinx.android.synthetic.main.content_event.view.eventLocationLinearLayout
import kotlinx.android.synthetic.main.content_event.view.eventName
import kotlinx.android.synthetic.main.content_event.view.eventOrganiserDescription
import kotlinx.android.synthetic.main.content_event.view.eventTimingLinearLayout
import kotlinx.android.synthetic.main.content_event.view.imageMap
import kotlinx.android.synthetic.main.content_event.view.eventImage
import kotlinx.android.synthetic.main.content_event.view.feedbackBtn
import kotlinx.android.synthetic.main.content_event.view.feedbackRv
import kotlinx.android.synthetic.main.content_event.view.nestedContentEventScroll
import kotlinx.android.synthetic.main.content_event.view.noFeedBackTv
import kotlinx.android.synthetic.main.content_event.view.seeMore
import kotlinx.android.synthetic.main.content_event.view.seeMoreOrganizer
import kotlinx.android.synthetic.main.content_event.view.sessionContainer
import kotlinx.android.synthetic.main.content_event.view.sessionsRv
import kotlinx.android.synthetic.main.content_event.view.speakerRv
import kotlinx.android.synthetic.main.content_event.view.speakersContainer
import kotlinx.android.synthetic.main.content_event.view.sponsorsRecyclerView
import kotlinx.android.synthetic.main.content_event.view.sponsorsSummaryContainer
import kotlinx.android.synthetic.main.fragment_event.view.buttonTickets
import kotlinx.android.synthetic.main.fragment_event.view.eventErrorCard
import kotlinx.android.synthetic.main.fragment_event.view.container
import kotlinx.android.synthetic.main.content_fetching_event_error.view.retry
import kotlinx.android.synthetic.main.dialog_feedback.view.feedback
import kotlinx.android.synthetic.main.dialog_feedback.view.feedbackTextInputLayout
import kotlinx.android.synthetic.main.dialog_feedback.view.feedbackrating
import kotlinx.android.synthetic.main.fragment_event.eventCoordinatorLayout
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.common.SessionClickListener
import org.fossasia.openevent.general.common.SpeakerClickListener
import org.fossasia.openevent.general.databinding.FragmentEventBinding
import org.fossasia.openevent.general.event.EventUtils.loadMapUrl
import org.fossasia.openevent.general.event.feedback.FeedbackRecyclerAdapter
import org.fossasia.openevent.general.event.topic.SimilarEventsFragment
import org.fossasia.openevent.general.sessions.SessionRecyclerAdapter
import org.fossasia.openevent.general.social.SocialLinksFragment
import org.fossasia.openevent.general.speakers.SpeakerRecyclerAdapter
import org.fossasia.openevent.general.sponsor.SponsorClickListener
import org.fossasia.openevent.general.sponsor.SponsorRecyclerAdapter
import org.fossasia.openevent.general.utils.Utils
import org.fossasia.openevent.general.utils.extensions.nonNull
import org.fossasia.openevent.general.utils.nullToEmpty
import org.fossasia.openevent.general.utils.stripHtml
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.Currency
import org.fossasia.openevent.general.utils.Utils.setToolbar
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar

const val EVENT_ID = "eventId"
const val EVENT_TOPIC_ID = "eventTopicId"
const val EVENT_LOCATION = "eventLocation"

class EventDetailsFragment : Fragment() {
    private val eventViewModel by viewModel<EventDetailsViewModel>()
    private val safeArgs: EventDetailsFragmentArgs by navArgs()
    private val feedbackAdapter = FeedbackRecyclerAdapter()
    private val speakersAdapter = SpeakerRecyclerAdapter()
    private val sponsorsAdapter = SponsorRecyclerAdapter()
    private val sessionsAdapter = SessionRecyclerAdapter()

    private lateinit var rootView: View
    private lateinit var binding: FragmentEventBinding
    private var eventTopicId: Long? = null
    private var eventLocation: String? = null
    private lateinit var eventShare: Event
    private var currency: String? = null
    private val LINE_COUNT: Int = 3
    private val LINE_COUNT_ORGANIZER: Int = 2
    private var menuActionBar: Menu? = null
    private var title: String = ""
    private var runOnce: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventViewModel.event
            .nonNull()
            .observe(this, Observer {
                loadEvent(it)
                eventShare = it
                title = eventShare.name

                // Update favorite icon and external event url menu option
                activity?.invalidateOptionsMenu()

                if (runOnce) {
                    loadSocialLinksFragment()
                    loadSimilarEventsFragment()
                }
                runOnce = false

                Timber.d("Fetched events of id ${safeArgs.eventId}")
                showEventErrorScreen(false)
                setHasOptionsMenu(true)
            })
        eventViewModel.loadEventFeedback(safeArgs.eventId)
        eventViewModel.fetchEventSpeakers(safeArgs.eventId)
        val allSessions = eventViewModel.eventSessions.value
        if (allSessions == null) {
            eventViewModel.fetchEventSessions(safeArgs.eventId)
        } else {
            sessionsAdapter.addAll(allSessions)
            rootView.sessionContainer.isVisible = allSessions.isNotEmpty()
        }
        eventViewModel.fetchEventSponsors(safeArgs.eventId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_event, container, false)
        rootView = binding.root
        setToolbar(activity)
        setHasOptionsMenu(true)

        rootView.feedbackRv.layoutManager = LinearLayoutManager(context)
        rootView.feedbackRv.adapter = feedbackAdapter

        rootView.buttonTickets.setOnClickListener {
            loadTicketFragment()
        }

        eventViewModel.popMessage
            .nonNull()
            .observe(viewLifecycleOwner, Observer {
                rootView.snackbar(it)
                showEventErrorScreen(it == getString(R.string.error_fetching_event_message))
            })

        eventViewModel.eventFeedback.observe(viewLifecycleOwner, Observer {
            feedbackAdapter.addAll(it)
            if (it.isEmpty()) {
                rootView.feedbackRv.isVisible = false
                rootView.noFeedBackTv.isVisible = true
            } else {
                rootView.feedbackRv.isVisible = true
                rootView.noFeedBackTv.isVisible = false
            }
        })

        eventViewModel.submittedFeedback
            .nonNull()
            .observe(viewLifecycleOwner, Observer {
                feedbackAdapter.add(it)
                rootView.feedbackRv.isVisible = true
                rootView.noFeedBackTv.isVisible = false
            })

        rootView.feedbackBtn.setOnClickListener {
            checkForAuthentication()
        }

        eventViewModel.loadEvent(safeArgs.eventId)
        rootView.retry.setOnClickListener {
            eventViewModel.loadEvent(safeArgs.eventId)
        }

        val speakerClickListener: SpeakerClickListener = object : SpeakerClickListener {
            override fun onClick(speakerId: Long) {
                findNavController(rootView).navigate(EventDetailsFragmentDirections
                    .actionEventDetailsToSpeaker(speakerId))
            }
        }

        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rootView.speakerRv.layoutManager = linearLayoutManager
        rootView.speakerRv.adapter = speakersAdapter
        speakersAdapter.apply {
            onSpeakerClick = speakerClickListener
        }

        val sponsorLinearLayoutManager = LinearLayoutManager(context)
        sponsorLinearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rootView.sponsorsRecyclerView.layoutManager = sponsorLinearLayoutManager
        rootView.sponsorsRecyclerView.adapter = sponsorsAdapter

        val sponsorClickListener: SponsorClickListener = object : SponsorClickListener {
            override fun onClick() {
                moveToSponsorSection()
            }
        }

        sponsorsAdapter.apply {
            onSponsorClick = sponsorClickListener
        }

        rootView.sponsorsSummaryContainer.setOnClickListener {
            moveToSponsorSection()
        }

        // Set toolbar title to event name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.nestedContentEventScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (scrollY > rootView.eventName.height + rootView.eventImage.height)
                    /*Toolbar title set to name of Event if scrolled more than
                    combined height of eventImage and eventName views*/
                    setToolbar(activity, title)
                else
                    // Toolbar title set to an empty string
                    setToolbar(activity)
            }
        }
        val linearLayoutManagerSpeakers = LinearLayoutManager(context)
        linearLayoutManagerSpeakers.orientation = LinearLayoutManager.HORIZONTAL

        rootView.speakerRv.layoutManager = linearLayoutManagerSpeakers
        rootView.speakerRv.adapter = speakersAdapter

        eventViewModel.loadEventSpeakers(safeArgs.eventId).observe(viewLifecycleOwner, Observer {
            speakersAdapter.addAll(it)
            if (it.isEmpty()) {
                rootView.speakersContainer.visibility = View.GONE
            } else {
                rootView.speakersContainer.visibility = View.VISIBLE
            }
        })

        val sessionClickListener: SessionClickListener = object : SessionClickListener {
            override fun onClick(sessionId: Long) {
                findNavController(rootView).navigate(EventDetailsFragmentDirections
                    .actionEventDetailsToSession(sessionId))
            }
        }

        val linearLayoutManagerSessions = LinearLayoutManager(context)
        linearLayoutManagerSessions.orientation = LinearLayoutManager.HORIZONTAL

        rootView.sessionsRv.layoutManager = linearLayoutManagerSessions
        rootView.sessionsRv.adapter = sessionsAdapter
        sessionsAdapter.apply {
            onSessionClick = sessionClickListener
        }

        eventViewModel.eventSessions.observe(viewLifecycleOwner, Observer {
            sessionsAdapter.addAll(it)
            if (it.isEmpty()) {
                rootView.sessionContainer.visibility = View.GONE
            } else {
                rootView.sessionContainer.visibility = View.VISIBLE
            }
        })

        eventViewModel.loadEventSponsors(safeArgs.eventId).observe(viewLifecycleOwner, Observer { sponsors ->
            sponsorsAdapter.addAll(sponsors)
            rootView.sponsorsSummaryContainer.visibility = if (sponsors.isEmpty()) View.GONE else View.VISIBLE
            sponsorsAdapter.notifyDataSetChanged()
        })

        return rootView
    }

    private fun loadEvent(event: Event) {
        val startsAt = EventUtils.getEventDateTime(event.startsAt, event.timezone)
        val endsAt = EventUtils.getEventDateTime(event.endsAt, event.timezone)
        binding.event = event
        binding.executePendingBindings()

        // Organizer Section
        if (!event.organizerName.isNullOrEmpty()) {
            val organizerDescriptionListener = View.OnClickListener {
                if (rootView.seeMoreOrganizer.text == getString(R.string.see_more)) {
                    rootView.seeMoreOrganizer.text = getString(R.string.see_less)
                    rootView.eventOrganiserDescription.minLines = 0
                    rootView.eventOrganiserDescription.maxLines = Int.MAX_VALUE
                } else {
                    rootView.seeMoreOrganizer.text = getString(R.string.see_more)
                    rootView.eventOrganiserDescription.setLines(3)
                }
            }

            rootView.eventOrganiserDescription.post {
                if (rootView.eventOrganiserDescription.lineCount > LINE_COUNT_ORGANIZER) {
                    rootView.seeMoreOrganizer.isVisible = true
                    // Set up toggle organizer description
                    rootView.seeMoreOrganizer.setOnClickListener(organizerDescriptionListener)
                    rootView.eventOrganiserDescription.setOnClickListener(organizerDescriptionListener)
                }
            }
        }
        currency = Currency.getInstance(event.paymentCurrency ?: "USD").symbol
        // About event on-click
        val aboutEventOnClickListener = View.OnClickListener {
            findNavController(rootView).navigate(EventDetailsFragmentDirections
                .actionEventDetailsToAboutEvent(safeArgs.eventId))
        }

        // Event Description Section
        val description = event.description.nullToEmpty().stripHtml()
        if (!description.isNullOrEmpty()) {

            rootView.eventDescription.post {
                if (rootView.eventDescription.lineCount > LINE_COUNT) {
                    rootView.seeMore.isVisible = true
                    // start about fragment
                    rootView.eventDescription.setOnClickListener(aboutEventOnClickListener)
                    rootView.seeMore.setOnClickListener(aboutEventOnClickListener)
                }
            }
        }

        // load location to map
        val mapClickListener = View.OnClickListener { startMap(event) }

        val locationNameIsEmpty = event.locationName.isNullOrEmpty()
        if (!locationNameIsEmpty) {
            rootView.imageMap.setOnClickListener(mapClickListener)
            rootView.eventLocationLinearLayout.setOnClickListener(mapClickListener)

            Picasso.get()
                    .load(eventViewModel.loadMap(event))
                    .placeholder(R.drawable.ic_map_black)
                    .error(R.drawable.ic_map_black)
                    .into(rootView.imageMap)
        }

        // Date and Time section
        rootView.eventDateDetailsFirst.text = EventUtils.getFormattedEventDateTimeRange(startsAt, endsAt)
        rootView.eventDateDetailsSecond.text = EventUtils.getFormattedEventDateTimeRangeSecond(startsAt, endsAt)

        // Similar Events Section
        if (event.eventTopic != null || !event.locationName.isNullOrBlank() ||
            !event.searchableLocationName.isNullOrBlank()) {
            similarEventsContainer.isVisible = true
            eventTopicId = event.eventTopic?.id ?: 0
            eventLocation =
                if (event.searchableLocationName.isNullOrBlank()) event.locationName
                else event.searchableLocationName
        }

        // Add event to Calendar
        val dateClickListener = View.OnClickListener { startCalendar(event) }
        rootView.eventTimingLinearLayout.setOnClickListener(dateClickListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            R.id.add_to_calendar -> {
                // Add event to Calendar
                startCalendar(eventShare)
                true
            }
            R.id.report_event -> {
                reportEvent(eventShare)
                true
            }
            R.id.open_external_event_url -> {
                eventShare.externalEventUrl?.let { Utils.openUrl(requireContext(), it) }
                true
            }
            R.id.favorite_event -> {
                eventViewModel.setFavorite(safeArgs.eventId, !(eventShare.favorite))
                true
            }
            R.id.event_share -> {
                EventUtils.share(eventShare, rootView.eventImage)
                return true
            }
            R.id.open_faqs -> {
                findNavController(rootView).navigate(EventDetailsFragmentDirections
                    .actionEventDetailsToFaq(safeArgs.eventId))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCalendar(event: Event) {
        val intent = Intent(Intent.ACTION_INSERT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra(CalendarContract.Events.TITLE, event.name)
        intent.putExtra(CalendarContract.Events.DESCRIPTION, event.description?.stripHtml())
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.locationName)
        intent.putExtra(CalendarContract.Events.CALENDAR_TIME_ZONE, event.timezone)
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
            EventUtils.getTimeInMilliSeconds(event.startsAt, event.timezone))
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
            EventUtils.getTimeInMilliSeconds(event.endsAt, event.timezone))
        startActivity(intent)
    }

    private fun reportEvent(event: Event) {
        val email = "support@eventyay.com"
        val subject = "Report of ${event.name} (${event.identifier})"
        val body = "Let us know what's wrong"
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)

        startActivity(Intent.createChooser(emailIntent, "Chooser Title"))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.event_details, menu)
        menuActionBar = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (::eventShare.isInitialized) {
            if (eventShare.externalEventUrl == null) {
                menu.findItem(R.id.open_external_event_url).isVisible = false
            }
            setFavoriteIconFilled(eventShare.favorite)
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun loadTicketFragment() {
        findNavController(rootView).navigate(EventDetailsFragmentDirections
            .actionEventDetailsToTickets(safeArgs.eventId, currency))
    }

    private fun loadSocialLinksFragment() {
        // Initialise SocialLinks Fragment
        val socialLinksFragemnt = SocialLinksFragment()
        val bundle = Bundle()
        bundle.putLong(EVENT_ID, safeArgs.eventId)
        socialLinksFragemnt.arguments = bundle
        socialLinksFragemnt.setErrorSnack {
            eventCoordinatorLayout.longSnackbar(it)
        }
        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.frameContainerSocial, socialLinksFragemnt).commit()
    }

    private fun loadSimilarEventsFragment() {
        // Initialise SimilarEvents Fragment
        val similarEventsFragment = SimilarEventsFragment()
        val bundle = Bundle()
        bundle.putLong(EVENT_ID, safeArgs.eventId)
        eventTopicId?.let { bundle.putLong(EVENT_TOPIC_ID, it) }
        eventLocation?.let { bundle.putString(EVENT_LOCATION, it) }
        similarEventsFragment.arguments = bundle
        similarEventsFragment.setErrorSnack {
            eventCoordinatorLayout.longSnackbar(it)
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.frameContainerSimilarEvents, similarEventsFragment).commit()
    }

    private fun startMap(event: Event) {
        // start map intent
        val mapUrl = loadMapUrl(event)
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
        val packageManager = activity?.packageManager
        if (packageManager != null && mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
    }

    private fun setFavoriteIconFilled(filled: Boolean) {
        val id = when {
            filled -> R.drawable.ic_baseline_favorite_white
            else -> R.drawable.ic_baseline_favorite_border_white
        }
        menuActionBar?.findItem(R.id.favorite_event)?.icon = ContextCompat.getDrawable(requireContext(), id)
    }

    private fun showEventErrorScreen(show: Boolean) {
        rootView.container.isVisible = !show
        rootView.eventErrorCard.isVisible = show
        val menuItemSize = menuActionBar?.size() ?: 0
        for (i in 0 until menuItemSize) {
            menuActionBar?.getItem(i)?.isVisible = !show
        }
    }

    private fun checkForAuthentication() {
        if (eventViewModel.isLoggedIn())
            writeFeedback()
        else {
            rootView.nestedContentEventScroll.longSnackbar(getString(R.string.log_in_first))
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        findNavController(rootView).navigate(EventDetailsFragmentDirections
            .actionEventDetailsToLogin(getString(R.string.log_in_first)))
    }

    private fun writeFeedback() {
        val layout = layoutInflater.inflate(R.layout.dialog_feedback, null)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.submit_feedback))
            .setView(layout)
            .setPositiveButton(getString(R.string.submit)) { _, _ ->
                eventViewModel.submitFeedback(layout.feedback.text.toString(),
                    layout.feedbackrating.rating,
                    safeArgs.eventId)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }

            .show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        layout.feedback.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                    if (layout.feedback.text.toString().isNotEmpty()) {
                        layout.feedbackTextInputLayout.error = null
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        layout.feedbackTextInputLayout.isErrorEnabled = false
                    } else {
                        layout.feedbackTextInputLayout.error = "Can't be Empty"
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { /*Implement here*/ }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { /*Implement here*/ }
            })
    }

    private fun moveToSponsorSection() {
        findNavController(rootView).navigate(EventDetailsFragmentDirections
            .actionEventDetailsToSponsor(safeArgs.eventId))
    }

    override fun onDestroy() {
        Picasso.get().cancelRequest(rootView.eventImage)
        super.onDestroy()
    }
}
