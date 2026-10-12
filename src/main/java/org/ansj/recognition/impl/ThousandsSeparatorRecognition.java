package org.ansj.recognition.impl;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.domain.TermNature;
import org.ansj.domain.TermNatures;
import org.ansj.recognition.Recognition;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/** Recognizes decimal numbers written with a thousands separator. */
public class ThousandsSeparatorRecognition implements Recognition {

    private static final long serialVersionUID = 1L;
    private static final TermNatures THOUSANDS_NATURES = new TermNatures(new TermNature("thousands_separator", 1));
    private static final Pattern GROUP_SEGMENT = Pattern.compile("[0-9]{3}(?:[.][0-9]+)?");

    private String separator = ",";
    private Pattern groupedNumber;

    public ThousandsSeparatorRecognition() {
        updatePattern();
    }

    public ThousandsSeparatorRecognition(String separator) {
        setSeparator(separator);
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        if (separator == null || separator.length() != 1 || Character.isDigit(separator.charAt(0))) {
            throw new IllegalArgumentException("The thousands separator must be one non-digit character.");
        }
        this.separator = separator;
        updatePattern();
    }

    @Override
    public void recognition(Result result) {
        List<Term> terms = result.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            Term first = terms.get(i);
            if (first.getOffe() < 0) {
                continue;
            }

            if (groupedNumber.matcher(first.getName()).matches()) {
                first.updateTermNaturesAndNature(THOUSANDS_NATURES);
                continue;
            }
            if (!isDigits(first.getName()) || first.getName().length() > 3) {
                continue;
            }

            int cursor = i;
            int groups = 0;
            boolean decimal = false;
            while (cursor + 2 < terms.size()) {
                Term separatorTerm = terms.get(cursor + 1);
                if (!separator.equals(separatorTerm.getName())) {
                    break;
                }

                int groupStart = cursor + 2;
                int groupEnd = groupStart;
                String group = terms.get(groupStart).getName();
                if (isDigits(group) && group.length() == 3
                        && groupEnd + 2 < terms.size()
                        && ".".equals(terms.get(groupEnd + 1).getName())
                        && isDigits(terms.get(groupEnd + 2).getName())) {
                    group += "." + terms.get(groupEnd + 2).getName();
                    groupEnd += 2;
                }
                if (!GROUP_SEGMENT.matcher(group).matches()) {
                    break;
                }

                first.merage(separatorTerm);
                separatorTerm.setOffe(-1);
                for (int part = groupStart; part <= groupEnd; part++) {
                    first.merage(terms.get(part));
                    terms.get(part).setOffe(-1);
                }
                first.updateTermNaturesAndNature(THOUSANDS_NATURES);
                cursor = groupEnd;
                groups++;
                decimal = group.indexOf('.') >= 0;
                if (decimal) {
                    break;
                }
            }
            if (groups > 0) {
                i = cursor;
            }
        }

        for (Iterator<Term> iterator = terms.iterator(); iterator.hasNext();) {
            if (iterator.next().getOffe() < 0) {
                iterator.remove();
            }
        }
    }

    private void updatePattern() {
        String groupedInteger = "[0-9]{1,3}(?:" + Pattern.quote(separator) + "[0-9]{3})+";
        groupedNumber = Pattern.compile(groupedInteger + "(?:\\.[0-9]+)?");
    }

    private static boolean isDigits(String value) {
        return value != null && value.matches("[0-9]+");
    }
}
