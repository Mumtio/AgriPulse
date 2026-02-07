// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

	dependencies  {
	   classpath(libs.gradle)
	}
}

allprojects {

}

// List of plugins used in the app, but do not apply for the root project
plugins {
    alias(libs.plugins.android.application) apply false
}