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


import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ProfileTests {

    public static final Gson gson = new Gson();
    public static final String SERVER_RESPONSE_JSON = "{\"login\":\"Kredwi\"," +
            "\"id\":74652542,\"node_id\":\"MDQ6VXNlcjc0NjUyNTQy\"," +
            "\"gravatar_id\":\"\",\"type\":\"User\",\"user_view_type\":\"public\"," +
            "\"site_admin\":false,\"name\":\"Kredwi\",\"company\":\"None\"," +
            "\"blog\":\"kredwi.ru\",\"location\":\"Home\",\"email\":null," +
            "\"hireable\":null,\"bio\":\"N\\\\A\",\"twitter_username\":null," +
            "\"public_repos\":19,\"public_gists\":0,\"followers\":0,\"following\":0," +
            "\"created_at\":\"2020-11-18T09:37:40Z\"," +
            "\"updated_at\":\"2026-03-28T15:44:15Z\"}";

    @Test
    public void serializableTest() {
        Profile profile = gson.fromJson(SERVER_RESPONSE_JSON, Profile.class);
        assertEquals("Kredwi", profile.getLogin());
        assertEquals("N\\A", profile.getBio());
        assertEquals("MDQ6VXNlcjc0NjUyNTQy", profile.getNodeId());
        assertEquals(19, profile.getPublicRepos());
        assertEquals("2020-11-18T09:37:40Z", profile.getCreatedAt());
        assertFalse(profile.isSiteAdmin());
        assertEquals("public", profile.getUserViewType());
    }

}
