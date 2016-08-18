# Random Restaurant Generator 

<a href="https://play.google.com/store/apps/details?id=com.chris.randomrestaurantgenerator">
<img alt="Get it on Google Play" src="http://steverichey.github.io/google-play-badge-svg/img/en_get.svg" />
</a>

It uses the [Yelp API](https://www.yelp.com/developers) to query for restaurants and uses [Google Play Services location API](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary). It also uses [Google Maps Android API](https://developers.google.com/maps/documentation/android-api/) for displaying the location of a restaurant.

## Description

Deciding where to eat takes forever. Either your friends / family can never agree on where to go, or you're feeling indecisive and can't pick. With Random Restaurant Generator, you don't have to ask anyone anything or worry about what to pick. Random Restaurant Generator searches for places to eat near you and picks one at random. 

If you don't like the place it finds, swipe it to the left to get a new one. Found one you like? Swipe it to the right to open it in Yelp and check out more info. Got a place that you're thinking about, but not quite sure? Add it to your saved list by clicking the bookmark icon to access it later.

Features:
 - Filter your searches by entering a category or type of food you like (perfect for vegans).
 - Ability to use your GPS for places near you or enter a location manually to search a specific area.
 - Save restaurants to access them later if you want to narrow down your choices.
 - Ability to open the restaurant's page in Yelp for more information.
 - Displays a mini Google Maps to show where the restaurant is and to get quick directions to it.
 
## Contributing

Please feel free to submit a pull request if you have any improvements or suggestions. If you run into any issues, please do submit them and give feedback wherever you can.

## Building

Building is quite simple with gradle:

```shell
git clone https://github.com/christarazi/random-restaurant-generator.git
cd random-restaurant-generator/
./gradlew build
```
