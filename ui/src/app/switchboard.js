import $ from "jquery";
import React from "react";

const DOMAIN = 'http://localhost:9010/switchboard/rest';
export default class Switchboard extends React.Component {

    static requestServices(objectId) {
        let url = `${DOMAIN}/object/${objectId}/services`;
        return $.get({
            url: url
        });
    }

    static requestDeployment(serviceName, objectId) {
        let url = `${DOMAIN}/exec/${serviceName}`;
        return $.post({
            url: url,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                params: [{
                    value: objectId,
                    type: "file",
                    name: "untokinput",
                    params: [{"language": "nld"}]
                }]
            })
        })
    }

}
