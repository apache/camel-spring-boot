/*
 * Copyright 2005-2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.springboot.patch;

import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.apache.camel.springboot.patch.extensions.ZipWagon;

//@Component(role = WagonProvider.class)
public class ZipWagonProvider implements WagonProvider {

    @Override
    public Wagon lookup(String roleHint) throws Exception {
        if (roleHint.equals("zip")) {
            return new ZipWagon();
        }
        return null;
    }

    @Override
    public void release(Wagon wagon) {
    }

}
