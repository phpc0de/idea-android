/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.android.dom.menu;

import com.android.resources.ResourceFolderType;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.android.dom.ResourceFolderTypeDomFileDescription;
import org.jetbrains.annotations.NotNull;

public class MenuDomFileDescription extends ResourceFolderTypeDomFileDescription<Menu> {
  public MenuDomFileDescription() {
    super(Menu.class, ResourceFolderType.MENU, "menu");
  }

  public static boolean isMenuFile(@NotNull XmlFile file) {
    return isFileInResourceFolderType(file, ResourceFolderType.MENU);
  }
}
