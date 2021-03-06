package io.kurumi.ntt.listeners.extra;

import io.kurumi.ntt.td.TdApi.User;
import io.kurumi.ntt.td.client.TdFunction;
import io.kurumi.ntt.td.model.TMsg;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class TdDnsLookup extends TdFunction {

    @Override
    public String functionName() {

        return "dns";

    }

    @Override
    public void onFunction(User user, TMsg msg, String function, String[] params) {

        if (params.length < 2) {

            sendText(msg, "/dns <type> <domain>");

            return;

        }

        int type = Type.A;

        String domain;

        if (params.length == 2) {

            type = Type.value(params[0]);

            domain = params[1];

            if (type < 0) {

                sendHTML(msg, getLocale(user).DNS_TYPE_INVALID);

                return;

            }

        } else {

            domain = params[0];

        }

        Lookup lookup;

        try {

            lookup = new Lookup(domain, type);

        } catch (TextParseException e) {

            sendHTML(msg, getLocale(user).DNS_DOMAIN_INVALID);

            return;

        }

        lookup.run();

        if (lookup.getResult() != Lookup.SUCCESSFUL) {

            sendText(msg, lookup.getErrorString());

            return;

        }

        Record[] records = lookup.getAnswers();

        TextBuilder message = new TextBuilder();

        if (records.length == 0) {

            message.text(getLocale(user).DNS_NOT_FOUND);

        } else {

            for (Record record : records) {

                message.text("\n").text(Type.string(record.getType())).text(" ").code(record.rdataToString());

            }

        }

        send(msg.sendText(message));

    }

}
