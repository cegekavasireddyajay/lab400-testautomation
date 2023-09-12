package ui.Helper;

import lombok.SneakyThrows;

import java.net.InetAddress;

public class Network {
    private static NetworkEnum detectedNetwork;

    @SneakyThrows
    private static NetworkEnum getDetectedNetwork() {
        if(detectedNetwork == null) {
            InetAddress address = InetAddress.getByName("b-bot.dev.viollier.ch");
            switch(address.getHostAddress()) {
                case "172.16.59.152":
                    detectedNetwork = NetworkEnum.CEGEKA;
                    break;
                case "10.16.80.152":
                    detectedNetwork = NetworkEnum.VIOLLIER;
                    break;
                default:
                    throw new RuntimeException("Could not resolve network. Address: " + address + ", HostAddress: " + address.getHostAddress());
            }
        }
        return detectedNetwork;
    }

    public static String select(String cegeka, String viollier) {
        return getDetectedNetwork().select(cegeka, viollier);
    }

    private enum NetworkEnum {
        CEGEKA {
            @Override
            public String select(String cegeka, String viollier) {
                return cegeka;
            }
        },
        VIOLLIER {
            @Override
            public String select(String cegeka, String viollier) {
                return viollier;
            }
        };

        public abstract String select(String cegeka, String viollier);
    }
}
