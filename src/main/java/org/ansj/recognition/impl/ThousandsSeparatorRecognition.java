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
        if (separator == null || separator.length() != 1 || Character.isDigit(separator.charAt(0))
                || ".".equals(separator)) {
            throw new IllegalArgumentException("The thousands separator must be one non-digit character other than a period.");
        }
        this.separator = separator;
        updatePattern();
    }

    @Override
    public void recognition(Result result) {
        List<Term> terms = result.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            Term first = terms.get(i);
            if (first.getOffe() < 0 || !startsWithDigit(first.getName()) || !isNumberPart(first.getName())) {
                continue;
            }

            StringBuilder value = new StringBuilder();
            int cursor = i;
            Term previous = null;
            while (cursor < terms.size()) {
                Term part = terms.get(cursor);
                if (part.getOffe() < 0 || !isNumberPart(part.getName())
                        || (previous != null && !areAdjacent(previous, part))) {
                    break;
                }
                value.append(part.getName());
                previous = part;
                cursor++;
            }

            if (groupedNumber.matcher(value).matches()) {
                for (int part = i + 1; part < cursor; part++) {
                    first.merage(terms.get(part));
                    terms.get(part).setOffe(-1);
                }
                first.updateTermNaturesAndNature(THOUSANDS_NATURES);
            }
            i = cursor - 1;
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

    private boolean isNumberPart(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character < '0' || character > '9') && character != separator.charAt(0) && character != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithDigit(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) >= '0' && value.charAt(0) <= '9';
    }

    private static boolean areAdjacent(Term left, Term right) {
        return left.getOffe() + left.getName().length() == right.getOffe();
    }
}
