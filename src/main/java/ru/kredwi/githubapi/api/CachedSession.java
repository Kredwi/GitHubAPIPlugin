package ru.kredwi.githubapi.api;

/*-
 * #%L
 * GithubAPIPlugin
 * %%
 * Copyright (C) 2026 Kredwi
 * %%
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
 * #L%
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * {@code CachedSession} mutable cache data class for a save timestamp
 *
 * @param <T> session type
 * @author Kredwi
 * @since 1.2
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedSession<T> {

    /**
     * timestamp of save
     *
     */
    private long timestamp;

    /**
     * Saved session instance
     *
     */
    @Nullable
    private T object;

    /**
     * Static method for get a empty cache instance
     * </br>
     * <ul>
     *     <li>timestamp is current timestamp in ms</li>
     *     <li>object is {@code null}</li>
     * </ul>
     *
     * @return empty instance of {@code CachedSession}
     *
     */
    public static <P> CachedSession<P> empty() {
        return new CachedSession<>(System.currentTimeMillis(), null);
    }
}
