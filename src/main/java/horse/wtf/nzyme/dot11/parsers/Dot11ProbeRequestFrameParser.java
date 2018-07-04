/*
 *  This file is part of nzyme.
 *
 *  nzyme is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  nzyme is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with nzyme.  If not, see <http://www.gnu.org/licenses/>.
 */

package horse.wtf.nzyme.dot11.parsers;

import horse.wtf.nzyme.dot11.Dot11MetaInformation;
import horse.wtf.nzyme.dot11.MalformedFrameException;
import horse.wtf.nzyme.dot11.frames.Dot11ProbeRequestFrame;
import horse.wtf.nzyme.util.Tools;
import org.pcap4j.packet.Dot11ProbeRequestPacket;
import org.pcap4j.packet.IllegalRawDataException;

import java.text.Normalizer;

public class Dot11ProbeRequestFrameParser extends Dot11FrameParser<Dot11ProbeRequestFrame> {

    @Override
    protected Dot11ProbeRequestFrame doParse(byte[] payload, byte[] header, Dot11MetaInformation meta) throws IllegalRawDataException, MalformedFrameException {
        Dot11ProbeRequestPacket probeRequest = Dot11ProbeRequestPacket.newPacket(payload, 0, payload.length);

        if (probeRequest.getHeader() == null) {
            throw new MalformedFrameException("Malformed header in probe request packet. Skipping.");
        }

        String ssid;
        boolean broadcastProbe = false;
        if (probeRequest.getHeader().getSsid() != null) {
            // Check if the SSID is valid UTF-8 (might me malformed frame)
            if(!Tools.isValidUTF8(probeRequest.getHeader().getSsid().getRawData())) {
                throw new MalformedFrameException("Malformed SSID in probe request packet. Skipping. <1>");
            }

            ssid = Normalizer.normalize(probeRequest.getHeader().getSsid().getSsid(), Normalizer.Form.NFD);

            if (ssid.trim().isEmpty()) {
                ssid = "NULL";
                broadcastProbe = true;
            }
        } else {
            throw new MalformedFrameException("Malformed SSID in probe request packet. Skipping. <2>");
        }

        String requester;
        if (probeRequest.getHeader().getAddress2() != null) {
            requester = Normalizer.normalize(probeRequest.getHeader().getAddress2().toString(), Normalizer.Form.NFD);
        } else {
            throw new MalformedFrameException("Malformed SSID in probe request packet. Skipping. <3>");
        }

        return Dot11ProbeRequestFrame.create(requester, ssid, broadcastProbe, meta);
    }

}