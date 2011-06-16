/*
 * Copyright 2011 Stanley Shyiko
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
package com.github.shyiko.native2asciiplug;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">shyiko</a>
 * @since 03.02.2011
 */
public class Native2AsciiPlugBundle {

    private static Reference<ResourceBundle> bundle;

    @NonNls
    private static final String BUNDLE = "Native2AsciiPlugBundle";

    private Native2AsciiPlugBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = Native2AsciiPlugBundle.bundle != null ? Native2AsciiPlugBundle.bundle.get() : null;
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            Native2AsciiPlugBundle.bundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }
}