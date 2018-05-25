import React from "react";
import $ from "jquery";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = '/objects/_table/object';

export default class Dreamfactory extends React.Component {

    static getObjects(params) {
        return $.get({
            url: `${DOMAIN}${OBJECT}?${params}`,
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
            }
        });
    }

}