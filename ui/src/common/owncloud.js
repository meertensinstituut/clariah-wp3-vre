import React from "react";
import $ from "jquery";

const DOMAIN = 'http://localhost:8082/remote.php/webdav';

export default class Owncloud extends React.Component {

    static getUrl(filepath) {
        const url = `${DOMAIN}/${filepath}`;
        return $.get({
            url:url,
            contentType: "text/html",
            beforeSend: function( xhr ) {
                xhr.setRequestHeader ("Authorization", "Basic " + btoa('admin:admin'));
                xhr.setRequestHeader ("Content-Type", "text/html");
                // xhr.setRequestHeader ("Access-Control-Allow-Headers", "*");
            }
        });
    }

}