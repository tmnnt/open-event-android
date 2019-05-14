package org.fossasia.openevent.general.di

import androidx.paging.PagedList
import androidx.room.Room
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jasminb.jsonapi.ResourceConverter
import com.github.jasminb.jsonapi.retrofit.JSONAPIConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.fossasia.openevent.general.BuildConfig
import org.fossasia.openevent.general.OpenEventDatabase
import org.fossasia.openevent.general.about.AboutEventViewModel
import org.fossasia.openevent.general.attendees.Attendee
import org.fossasia.openevent.general.attendees.AttendeeApi
import org.fossasia.openevent.general.attendees.AttendeeId
import org.fossasia.openevent.general.attendees.AttendeeService
import org.fossasia.openevent.general.attendees.AttendeeViewModel
import org.fossasia.openevent.general.attendees.forms.CustomForm
import org.fossasia.openevent.general.auth.AuthApi
import org.fossasia.openevent.general.auth.AuthHolder
import org.fossasia.openevent.general.auth.AuthService
import org.fossasia.openevent.general.auth.EditProfileViewModel
import org.fossasia.openevent.general.auth.LoginViewModel
import org.fossasia.openevent.general.auth.ProfileViewModel
import org.fossasia.openevent.general.auth.RequestAuthenticator
import org.fossasia.openevent.general.auth.SignUp
import org.fossasia.openevent.general.auth.SignUpViewModel
import org.fossasia.openevent.general.auth.User
import org.fossasia.openevent.general.data.Network
import org.fossasia.openevent.general.data.Preference
import org.fossasia.openevent.general.event.Event
import org.fossasia.openevent.general.event.EventApi
import org.fossasia.openevent.general.event.EventDetailsViewModel
import org.fossasia.openevent.general.event.EventId
import org.fossasia.openevent.general.event.EventService
import org.fossasia.openevent.general.common.EventsDiffCallback
import org.fossasia.openevent.general.data.Resource
import org.fossasia.openevent.general.event.EventsListAdapter
import org.fossasia.openevent.general.event.EventsViewModel
import org.fossasia.openevent.general.event.feedback.Feedback
import org.fossasia.openevent.general.event.feedback.FeedbackApi
import org.fossasia.openevent.general.event.faq.EventFAQ
import org.fossasia.openevent.general.event.faq.EventFAQApi
import org.fossasia.openevent.general.event.location.EventLocation
import org.fossasia.openevent.general.event.location.EventLocationApi
import org.fossasia.openevent.general.event.subtopic.EventSubTopic
import org.fossasia.openevent.general.event.topic.EventTopic
import org.fossasia.openevent.general.event.topic.EventTopicApi
import org.fossasia.openevent.general.event.types.EventType
import org.fossasia.openevent.general.event.types.EventTypesApi
import org.fossasia.openevent.general.event.topic.SimilarEventsViewModel
import org.fossasia.openevent.general.favorite.FavoriteEventsRecyclerAdapter
import org.fossasia.openevent.general.favorite.FavoriteEventsViewModel
import org.fossasia.openevent.general.notification.Notification
import org.fossasia.openevent.general.notification.NotificationApi
import org.fossasia.openevent.general.notification.NotificationService
import org.fossasia.openevent.general.notification.NotificationViewModel
import org.fossasia.openevent.general.order.Charge
import org.fossasia.openevent.general.order.ConfirmOrder
import org.fossasia.openevent.general.order.Order
import org.fossasia.openevent.general.order.OrderApi
import org.fossasia.openevent.general.order.OrderCompletedViewModel
import org.fossasia.openevent.general.order.OrderDetailsViewModel
import org.fossasia.openevent.general.order.OrderService
import org.fossasia.openevent.general.order.OrdersUnderUserViewModel
import org.fossasia.openevent.general.paypal.Paypal
import org.fossasia.openevent.general.paypal.PaypalApi
import org.fossasia.openevent.general.search.GeoLocationViewModel
import org.fossasia.openevent.general.search.SearchLocationViewModel
import org.fossasia.openevent.general.search.SearchTimeViewModel
import org.fossasia.openevent.general.search.SearchViewModel
import org.fossasia.openevent.general.search.LocationService
import org.fossasia.openevent.general.search.SearchTypeViewModel
import org.fossasia.openevent.general.search.LocationServiceImpl
import org.fossasia.openevent.general.auth.SmartAuthViewModel
import org.fossasia.openevent.general.connectivity.MutableConnectionLiveData
import org.fossasia.openevent.general.event.SimilarEventsListAdapter
import org.fossasia.openevent.general.sessions.Session
import org.fossasia.openevent.general.sessions.SessionApi
import org.fossasia.openevent.general.sessions.SessionService
import org.fossasia.openevent.general.event.faq.EventFAQViewModel
import org.fossasia.openevent.general.sessions.SessionViewModel
import org.fossasia.openevent.general.sessions.microlocation.MicroLocation
import org.fossasia.openevent.general.sessions.sessiontype.SessionType
import org.fossasia.openevent.general.settings.SettingsViewModel
import org.fossasia.openevent.general.social.SocialLink
import org.fossasia.openevent.general.social.SocialLinkApi
import org.fossasia.openevent.general.social.SocialLinksService
import org.fossasia.openevent.general.social.SocialLinksViewModel
import org.fossasia.openevent.general.speakers.Speaker
import org.fossasia.openevent.general.speakers.SpeakerApi
import org.fossasia.openevent.general.speakers.SpeakerService
import org.fossasia.openevent.general.speakers.SpeakerViewModel
import org.fossasia.openevent.general.sponsor.Sponsor
import org.fossasia.openevent.general.sponsor.SponsorApi
import org.fossasia.openevent.general.sponsor.SponsorService
import org.fossasia.openevent.general.sponsor.SponsorsViewModel
import org.fossasia.openevent.general.ticket.Ticket
import org.fossasia.openevent.general.ticket.TicketApi
import org.fossasia.openevent.general.ticket.TicketId
import org.fossasia.openevent.general.ticket.TicketService
import org.fossasia.openevent.general.ticket.TicketsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

val commonModule = module {
    single { Preference() }
    single { Network() }
    factory<LocationService> { LocationServiceImpl(androidContext()) }
}

val apiModule = module {
    single {
        val retrofit: Retrofit = get()
        retrofit.create(EventApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(AuthApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(TicketApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(SocialLinkApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(EventTopicApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(AttendeeApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(OrderApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(PaypalApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(EventTypesApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(EventLocationApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(FeedbackApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(SpeakerApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(EventFAQApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(SessionApi::class.java)
    }

    single {
        val retrofit: Retrofit = get()
        retrofit.create(SponsorApi::class.java)
    }
    single {
        val retrofit: Retrofit = get()
        retrofit.create(NotificationApi::class.java)
    }

    factory { AuthHolder(get()) }
    factory { AuthService(get(), get(), get()) }

    factory { EventService(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SpeakerService(get(), get(), get()) }
    factory { SponsorService(get(), get(), get()) }
    factory { TicketService(get(), get()) }
    factory { SocialLinksService(get(), get()) }
    factory { AttendeeService(get(), get(), get()) }
    factory { OrderService(get(), get(), get()) }
    factory { SessionService(get(), get()) }
    factory { Resource() }
    factory { MutableConnectionLiveData() }
    factory { NotificationService(get()) }
}

val viewModelModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { EventsViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get()) }
    viewModel { EventDetailsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SessionViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { AttendeeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SearchLocationViewModel(get(), get()) }
    viewModel { SearchTimeViewModel(get()) }
    viewModel { SearchTypeViewModel(get(), get(), get()) }
    viewModel { TicketsViewModel(get(), get(), get(), get(), get()) }
    viewModel { AboutEventViewModel(get(), get()) }
    viewModel { EventFAQViewModel(get(), get()) }
    viewModel { SocialLinksViewModel(get(), get(), get()) }
    viewModel { FavoriteEventsViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SimilarEventsViewModel(get(), get()) }
    viewModel { OrderCompletedViewModel(get(), get()) }
    viewModel { OrdersUnderUserViewModel(get(), get(), get(), get()) }
    viewModel { OrderDetailsViewModel(get(), get(), get()) }
    viewModel { EditProfileViewModel(get(), get(), get()) }
    viewModel { GeoLocationViewModel(get()) }
    viewModel { SmartAuthViewModel() }
    viewModel { SpeakerViewModel(get(), get()) }
    viewModel { SponsorsViewModel(get(), get()) }
    viewModel { NotificationViewModel(get(), get(), get(), get()) }
}

val networkModule = module {

    single {
        val objectMapper = jacksonObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper
    }

    single {
        PagedList
            .Config
            .Builder()
            .setPageSize(10)
            .setInitialLoadSizeHint(10)
            .setEnablePlaceholders(false)
            .build()
    }

    single { RequestAuthenticator(get()) as Interceptor }

    single {
        val connectTimeout = 15 // 15s
        val readTimeout = 15 // 15s

        val builder = OkHttpClient().newBuilder()
            .connectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeout.toLong(), TimeUnit.SECONDS)
            .addInterceptor(get())
            .addNetworkInterceptor(StethoInterceptor())

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        builder.build()
    }

    single {
        val baseUrl = BuildConfig.DEFAULT_BASE_URL
        val objectMapper: ObjectMapper = get()
        val onlineApiResourceConverter = ResourceConverter(
            objectMapper, Event::class.java, User::class.java,
            SignUp::class.java, Ticket::class.java, SocialLink::class.java, EventId::class.java,
            EventTopic::class.java, Attendee::class.java, TicketId::class.java, Order::class.java,
            AttendeeId::class.java, Charge::class.java, Paypal::class.java, ConfirmOrder::class.java,
            CustomForm::class.java, EventLocation::class.java, EventType::class.java,
            EventSubTopic::class.java, Feedback::class.java, Speaker::class.java,
            Session::class.java, SessionType::class.java, MicroLocation::class.java,
            Sponsor::class.java, EventFAQ::class.java, Notification::class.java)

        Retrofit.Builder()
            .client(get())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(JSONAPIConverterFactory(onlineApiResourceConverter))
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .baseUrl(baseUrl)
            .build()
    }
}

val databaseModule = module {

    single {
        Room.databaseBuilder(androidApplication(),
            OpenEventDatabase::class.java, "open_event_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.eventDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.sessionDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.userDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.ticketDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.socialLinksDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.attendeeDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.eventTopicsDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.orderDao()
    }
    factory {
        val database: OpenEventDatabase = get()
        database.speakerWithEventDao()
    }
    factory {
        val database: OpenEventDatabase = get()
        database.speakerDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.sponsorWithEventDao()
    }

    factory {
        val database: OpenEventDatabase = get()
        database.sponsorDao()
    }
}

val fragmentsModule = module {

    factory { EventsDiffCallback() }

    scope(Scopes.EVENTS_FRAGMENT.toString()) {
        EventsListAdapter(get())
    }

    scope(Scopes.SIMILAR_EVENTS_FRAGMENT.toString()) {
        SimilarEventsListAdapter(get())
    }

    scope(Scopes.FAVORITE_FRAGMENT.toString()) {
        FavoriteEventsRecyclerAdapter(get())
    }

    scope(Scopes.SEARCH_RESULTS_FRAGMENT.toString()) {
        FavoriteEventsRecyclerAdapter(get())
    }
}
