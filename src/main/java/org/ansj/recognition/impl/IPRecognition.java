package org.ansj.recognition.impl;

import org.ansj.domain.Nature;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.recognition.Recognition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Recognizes syntactically valid IPv4 addresses. */
public class IPRecognition implements Recognition {

    private static final long serialVersionUID = 1L;
    private static final Nature IP_NATURE = new Nature("ip");

    @Override
    public void recognition(Result result) {
        List<Term> terms = result.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            if (term.getOffe() < 0) {
                continue;
            }

            if (isValidAddress(term.getName())) {
                if (!continuesAddress(terms, i + 1)) {
                    term.setNature(IP_NATURE);
                }
                continue;
            }

            List<Term> parts = new ArrayList<Term>(7);
            StringBuilder address = new StringBuilder();
            int cursor = i;
            boolean valid = true;
            for (int octet = 0; octet < 4; octet++) {
                if (cursor >= terms.size()) {
                    valid = false;
                    break;
                }
                Term number = terms.get(cursor);
                if (!isOctet(number.getName())) {
                    valid = false;
                    break;
                }
                if (octet > 0 && !areAdjacent(terms.get(cursor - 1), number)) {
                    valid = false;
                    break;
                }
                parts.add(number);
                address.append(number.getName());
                cursor++;

                if (octet < 3) {
                    if (cursor >= terms.size() || !".".equals(terms.get(cursor).getName())
                            || !areAdjacent(terms.get(cursor - 1), terms.get(cursor))) {
                        valid = false;
                        break;
                    }
                    parts.add(terms.get(cursor));
                    address.append('.');
                    cursor++;
                }
            }

            if (valid && isValidAddress(address.toString()) && !continuesAddress(terms, cursor)) {
                Term first = parts.get(0);
                for (int part = 1; part < parts.size(); part++) {
                    first.merage(parts.get(part));
                    parts.get(part).setOffe(-1);
                }
                first.setNature(IP_NATURE);
                i = cursor - 1;
            }
        }

        for (Iterator<Term> iterator = terms.iterator(); iterator.hasNext();) {
            if (iterator.next().getOffe() < 0) {
                iterator.remove();
            }
        }
    }

    private static boolean isValidAddress(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (!isOctet(octet)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOctet(String value) {
        if (value == null || !value.matches("[0-9]{1,3}")
                || (value.length() > 1 && value.charAt(0) == '0')) {
            return false;
        }
        return Integer.parseInt(value) <= 255;
    }

    private static boolean continuesAddress(List<Term> terms, int index) {
        return index > 0 && index < terms.size()
                && areAdjacent(terms.get(index - 1), terms.get(index))
                && terms.get(index).getName().startsWith(".");
    }

    private static boolean areAdjacent(Term left, Term right) {
        return left.getOffe() + left.getName().length() == right.getOffe();
    }
}
