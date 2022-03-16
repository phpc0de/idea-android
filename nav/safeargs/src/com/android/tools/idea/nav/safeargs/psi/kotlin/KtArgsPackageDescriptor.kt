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
package com.android.tools.idea.nav.safeargs.psi.kotlin

import com.android.ide.common.repository.GradleVersion
import com.android.tools.idea.nav.safeargs.index.NavDestinationData
import com.android.tools.idea.nav.safeargs.module.SafeArgsModuleInfo
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.alwaysTrue

/**
 * Args Kt package descriptor, which wraps and indirectly exposes a [LightArgsKtClass] class descriptor
 */
class KtArgsPackageDescriptor(
  private val containingModuleInfo: SafeArgsModuleInfo,
  private val navigationVersion: GradleVersion,
  fqName: FqName,
  val className: Name,
  private val destination: NavDestinationData,
  private val superTypesProvider: (PackageFragmentDescriptorImpl) -> Collection<KotlinType>,
  private val sourceElement: SourceElement,
  private val storageManager: StorageManager
) : PackageFragmentDescriptorImpl(containingModuleInfo.moduleDescriptor, fqName) {
  private val scope = storageManager.createLazyValue { SafeArgsModuleScope() }

  override fun getMemberScope(): MemberScope = scope()

  override fun getContainingDeclaration(): ModuleDescriptor {
    return containingModuleInfo.module.toDescriptor() ?: super.getContainingDeclaration()
  }

  private val safeArgsPackageDescriptor = this@KtArgsPackageDescriptor

  private inner class SafeArgsModuleScope : MemberScopeImpl() {
    private val lightClass = storageManager.createLazyValue {
      LightArgsKtClass(navigationVersion, className, destination, superTypesProvider(safeArgsPackageDescriptor),
                       sourceElement, safeArgsPackageDescriptor, storageManager)
    }

    override fun getContributedDescriptors(
      kindFilter: DescriptorKindFilter,
      nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
      return if ((kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK) && nameFilter(lightClass().name))) {
        listOf(lightClass())
      }
      else {
        emptyList()
      }
    }

    override fun getClassifierNames(): Set<Name> {
      return getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, alwaysTrue())
        .filterIsInstance<ClassifierDescriptor>()
        .mapTo(mutableSetOf()) { it.name }
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
      return if (lightClass().name == name) lightClass() else null
    }

    override fun printScopeStructure(p: Printer) {
      p.println(this::class.java.simpleName)
    }
  }
}