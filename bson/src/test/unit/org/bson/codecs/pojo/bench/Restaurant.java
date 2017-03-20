/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo.bench;

import org.bson.codecs.pojo.annotations.Property;

import java.util.List;

public final class Restaurant {

    @Property(name = "restaurant_id")
    private String restaurantId;

    private Address address;
    private String borough;
    private String cuisine;

    private List<Grade> grades;
    private String name;

    public Restaurant() {
    }


    public Restaurant(final String restaurantId, final Address address, final String borough, final String cuisine,
                      final List<Grade> grades, final String name) {
        this.restaurantId = restaurantId;
        this.address = address;
        this.borough = borough;
        this.cuisine = cuisine;
        this.grades = grades;
        this.name = name;
    }

    /**
     * Returns the restaurantId
     *
     * @return the restaurantId
     */
    public String getRestaurantId() {
        return restaurantId;
    }

    /**
     * Returns the address
     *
     * @return the address
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Returns the borough
     *
     * @return the borough
     */
    public String getBorough() {
        return borough;
    }

    /**
     * Returns the cuisine
     *
     * @return the cuisine
     */
    public String getCuisine() {
        return cuisine;
    }

    /**
     * Returns the grades
     *
     * @return the grades
     */
    public List<Grade> getGrades() {
        return grades;
    }

    /**
     * Returns the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Restaurant)) {
            return false;
        }

        Restaurant restaurant = (Restaurant) o;

        if (getRestaurantId() != null ? !getRestaurantId().equals(restaurant.getRestaurantId()) : restaurant.getRestaurantId() != null) {
            return false;
        }
        if (getAddress() != null ? !getAddress().equals(restaurant.getAddress()) : restaurant.getAddress() != null) {
            return false;
        }
        if (getBorough() != null ? !getBorough().equals(restaurant.getBorough()) : restaurant.getBorough() != null) {
            return false;
        }
        if (getCuisine() != null ? !getCuisine().equals(restaurant.getCuisine()) : restaurant.getCuisine() != null) {
            return false;
        }
        if (getGrades() != null ? !getGrades().equals(restaurant.getGrades()) : restaurant.getGrades() != null) {
            return false;
        }
        if (getName() != null ? !getName().equals(restaurant.getName()) : restaurant.getName() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getRestaurantId() != null ? getRestaurantId().hashCode() : 0;
        result = 31 * result + (getAddress() != null ? getAddress().hashCode() : 0);
        result = 31 * result + (getBorough() != null ? getBorough().hashCode() : 0);
        result = 31 * result + (getCuisine() != null ? getCuisine().hashCode() : 0);
        result = 31 * result + (getGrades() != null ? getGrades().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}
