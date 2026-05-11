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


import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * GitHub API Profile
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    public static final Profile EMPTY_PROFILE = new Profile();

    private String login;
    private int id;
    private String type;
    private String name;
    @Nullable
    private String company;
    @Nullable
    private String blog;
    @Nullable
    private String location;
    @Nullable
    private String email;
    @Nullable
    private String hireable;
    @Nullable
    private String bio;
    private int followers;
    private int following;

    @SerializedName("node_id")
    private String nodeId;
    @Nullable
    @SerializedName("twitter_username")
    private String twitterUsername;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;
    @SerializedName("public_repos")
    private int publicRepos;
    @SerializedName("public_gists")
    private int publicGists;
    @SerializedName("site_admin")
    private boolean siteAdmin;
    @Nullable
    @SerializedName("user_view_type")
    private String userViewType;
}
