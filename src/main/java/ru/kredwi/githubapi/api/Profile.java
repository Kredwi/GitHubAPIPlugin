package ru.kredwi.githubapi.api;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

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
