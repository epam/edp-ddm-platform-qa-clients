/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.bettercloud.vault.api.mounts.MountPayload;
import com.bettercloud.vault.api.mounts.MountType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MountOptions {

    private String pathSecretEngine;

    @Builder.Default
    private String description = "test not Trembita";

    @Builder.Default
    private MountType mountType = MountType.KEY_VALUE_V2;

    @Builder.Default
    private MountPayload mountPayload = new MountPayload();

}
