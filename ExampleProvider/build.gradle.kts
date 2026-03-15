dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "HDFilmCehennemi and Dizipal Providers"
    authors = listOf("Cloudstream Developer") // Replace with your GitHub username

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1 

    tvTypes = listOf("Movie", "TvSeries", "Anime")

    requiresResources = true
    language = "tr"

    // Optional: Add a general icon for your plugin collection
    iconUrl = "https://www.hdfilmcehennemi.nl/favicon.ico"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
