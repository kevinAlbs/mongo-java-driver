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

import java.util.List;


public final class Address {
    private String building;
    private List<Double> coord;
    private String street;
    private String zipcode;

    public Address(){
    }

    public Address(final String building, final List<Double> coord, final String street, final String zipcode) {
        this.building = building;
        this.coord = coord;
        this.street = street;
        this.zipcode = zipcode;
    }

    /**
     * Returns the building
     *
     * @return the building
     */
    public String getBuilding() {
        return building;
    }

    /**
     * Returns the coord
     *
     * @return the coord
     */
    public List<Double> getCoord() {
        return coord;
    }

    /**
     * Returns the street
     *
     * @return the street
     */
    public String getStreet() {
        return street;
    }

    /**
     * Returns the zipcode
     *
     * @return the zipcode
     */
    public String getZipcode() {
        return zipcode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address)) {
            return false;
        }

        Address address = (Address) o;

        if (getBuilding() != null ? !getBuilding().equals(address.getBuilding()) : address.getBuilding() != null) {
            return false;
        }
        if (getCoord() != null ? !getCoord().equals(address.getCoord()) : address.getCoord() != null) {
            return false;
        }
        if (getStreet() != null ? !getStreet().equals(address.getStreet()) : address.getStreet() != null) {
            return false;
        }
        if (getZipcode() != null ? !getZipcode().equals(address.getZipcode()) : address.getZipcode() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getBuilding() != null ? getBuilding().hashCode() : 0;
        result = 31 * result + (getCoord() != null ? getCoord().hashCode() : 0);
        result = 31 * result + (getStreet() != null ? getStreet().hashCode() : 0);
        result = 31 * result + (getZipcode() != null ? getZipcode().hashCode() : 0);
        return result;
    }
}
