/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("FormatUtil")

package com.android.tools.idea.util

import com.google.common.collect.Iterables

/**
 * Formats a message picking the format string depending on number of arguments
 *
 * @param values                       values that will be used as format argument.
 * @param oneElementMessage            message when only one value is in the list. Should accept one string argument.
 * @param twoOrThreeElementsMessage    message format when there's 2 or 3 values. Should accept two string arguments.
 * @param moreThenThreeElementsMessage message format for over 3 values. Should accept one string and one number.
 * @return formatted message string
 */
fun formatElementListString(
  values: Iterable<String>, oneElementMessage: String, twoOrThreeElementsMessage: String, moreThenThreeElementsMessage: String
): String {
  val size = Iterables.size(values)
  return when {
    // If there's 0 elements, some error happened
    size == 0 -> "<validation error>"
    size == 1 -> oneElementMessage.format(values.first())
    size <= 3 -> twoOrThreeElementsMessage.format(atMostTwo(values, size), values.last())
    else -> moreThenThreeElementsMessage.format(atMostTwo(values, size), size - 2)
  }
}

private fun atMostTwo(names: Iterable<String>, size: Int): String =
  names.take((size - 1).coerceAtMost(2)).joinToString(", ")
