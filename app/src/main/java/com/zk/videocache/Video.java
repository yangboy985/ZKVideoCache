package com.zk.videocache;

public enum Video {
    ORANGE_1("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4"), ORANGE_2("http://vjs.zencdn.net/v/oceans.mp4"), ORANGE_3("https://media.w3.org/2010/05/sintel/trailer.mp4"), ORANGE_4("http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4"), ORANGE_5("http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4"), ORANGE_6("http://c3s.vanmatt.com/dsp/video/video/u7499/d20200302/1583144772070.mp4");
    public final String url;

    Video(String url) {
        this.url = url;
    }

    public static String getUrlByIndex(int index) {
        String url = null;
        switch (index) {
            case 0:
                url = ORANGE_1.url;
                break;
            case 1:
                url = ORANGE_2.url;
                break;
            case 2:
                url = ORANGE_3.url;
                break;
            case 3:
                url = ORANGE_4.url;
                break;
            case 4:
                url = ORANGE_5.url;
                break;
            case 5:
                url = ORANGE_6.url;
                break;
            default:
                break;
        }
        return url;
    }

    private class Config {
        private static final String ROOT = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/";
    }
}