/*
 * Fonteo
 * Copyright (C) 2023  DomiRusz24
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.domirusz24.fonteo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

@Getter
@RequiredArgsConstructor
public class ExpandedCommandLine {

    private final CommandLine cmd;

    public <T> T getOrDefault(String option, T defaultValue) {
        if (!cmd.hasOption(option)) return defaultValue;
        String value = cmd.getOptionValue(option);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return (T) cmd.getParsedOptionValue(option);
        } catch (ParseException | ClassCastException e) {
            System.out.println("Incorrect parameter at option " + option + "! Defaulting to: " + defaultValue.toString());
            return defaultValue;
        }
    }

    public Number getOrDefaultNumber(String option, Number defaultValue) {
        return getOrDefault(option, defaultValue);
    }

    public <T> T getOrCrash(String option) {
        if (!cmd.hasOption(option)) throw new IllegalArgumentException();
        String value = cmd.getOptionValue(option);
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
        try {
            return (T) cmd.getParsedOptionValue(option);
        } catch (ParseException | ClassCastException e) {
            throw new IllegalArgumentException();
        }
    }
}
