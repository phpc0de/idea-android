/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentInfoEventBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.widget.FadingSnackbar
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class EventFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager
    private lateinit var eventInfoViewModel: EventInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context ?: return null
        eventInfoViewModel = viewModelProvider(viewModelFactory)

        val binding = FragmentInfoEventBinding.inflate(inflater, container, false).apply {
            viewModel = eventInfoViewModel
            setLifecycleOwner(this@EventFragment)
        }
        val snackbarLayout = requireActivity().findViewById<FadingSnackbar>(R.id.snackbar)
        setUpSnackbar(eventInfoViewModel.snackBarMessage, snackbarLayout, snackbarMessageManager)

        // TODO: launch filtered schedule
        // TODO: launch map
        // TODO: launch codelabs
        binding.eventSandbox.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventCodelabs.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventOfficehours.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventAfterhours.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        eventInfoViewModel.openUrlEvent.observe(this, Observer {
            val url = it?.getContentIfNotHandled() ?: return@Observer
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        })
    }
}

@BindingAdapter("countdownVisibility")
fun countdownVisibility(countdown: View, ignored: Boolean?) {
    countdown.visibility = if (TimeUtils.conferenceHasStarted()) GONE else VISIBLE
}
