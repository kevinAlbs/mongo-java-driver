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

import java.util.Date;

public final class Grade {

    private Date date;
    private String grade;
    private Integer score;

    public Grade() {
    }

    public Grade(final Date date, final String grade, final Integer score) {
        this.date = date;
        this.grade = grade;
        this.score = score;
    }

    /**
     * Returns the date
     *
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns the grade
     *
     * @return the grade
     */
    public String getGrade() {
        return grade;
    }

    /**
     * Returns the score
     *
     * @return the score
     */
    public Integer getScore() {
        return score;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Grade)) {
            return false;
        }

        Grade grade1 = (Grade) o;

        if (getDate() != null ? !getDate().equals(grade1.getDate()) : grade1.getDate() != null) {
            return false;
        }
        if (getGrade() != null ? !getGrade().equals(grade1.getGrade()) : grade1.getGrade() != null) {
            return false;
        }
        if (getScore() != null ? !getScore().equals(grade1.getScore()) : grade1.getScore() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getDate() != null ? getDate().hashCode() : 0;
        result = 31 * result + (getGrade() != null ? getGrade().hashCode() : 0);
        result = 31 * result + (getScore() != null ? getScore().hashCode() : 0);
        return result;
    }
}
