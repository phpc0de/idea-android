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

package com.google.samples.apps.iosched.ui.schedule.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.databinding.FragmentScheduleAgendaBinding
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.util.clearDecorations
import dagger.android.support.DaggerFragment
import org.threeten.bp.ZoneId
import javax.inject.Inject

class ScheduleAgendaFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ScheduleViewModel
    private lateinit var binding: FragmentScheduleAgendaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScheduleAgendaBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@ScheduleAgendaFragment)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider(viewModelFactory)
        binding.viewModel = viewModel
    }
}

@BindingAdapter(value = ["agendaItems", "timeZoneId"])
fun agendaItems(recyclerView: RecyclerView, list: List<Block>?, timeZoneId: ZoneId?) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = ScheduleAgendaAdapter()
    }
    val adapter = (recyclerView.adapter as ScheduleAgendaAdapter).apply {
        this.submitList(list ?: emptyList())
        this.timeZoneId = timeZoneId ?: ZoneId.systemDefault()
    }

    // Recreate the decoration used for the sticky date headers
    recyclerView.clearDecorations()
    if (list != null && list.isNotEmpty()) {
        recyclerView.addItemDecoration(
            ScheduleAgendaHeadersDecoration(recyclerView.context, list)
        )
    }
}
