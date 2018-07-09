import React from "react";
import Switchboard from "../common/switchboard";
import Form from "./form";
import StatePropsViewer from "../common/state-props-viewer";
import PropTypes from 'prop-types';
import Dreamfactory from "../common/dreamfactory";

/**
 * ServiceParams contains a json template from which a form is created.
 * Fields can be added according to min. and max. cardinality:
 * - Params with child params are duplicated
 * - Params without chi params get an extra element in param.value[]
 */
export default class Configurator extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            form: null,
            serviceParams: null
        };
        if (this.props.service !== undefined) {
            Switchboard.getParams(this.props.service).then((data) => {
                let form = this.createForm(data);
                Dreamfactory.getObject(this.props.file).then((objectData) => {
                    this.setFile(form, objectData);
                    this.setState({serviceParams: data, form: form});
                });
            });
        }

        this.change = this.change.bind(this);

    }

    createForm(serviceParams) {
        let form = {valid: false, params: []};
        serviceParams.params.forEach((param) => {
            let formParam = this.createFormParam(param, form);

            if (param.params) {
                formParam.params = [];
                param.params.forEach((childParam) => {
                    this.createFormParam(childParam, formParam);
                }, this);
            }
        }, this);
        return form;
    }

    createFormParam(cmdiParam, parent) {
        let formParam = Object.assign({}, cmdiParam);
        parent.params.push(formParam);
        formParam.value = [""];
        return formParam;
    }

    setFile(form, data) {
        let filePredicate = (p) => p.type === 'file';
        let params = form.params;
        let fileField = params.find(filePredicate);
        let fileIndex = params.findIndex(filePredicate);
        fileField.fileData = data;
        fileField.value = [data.id];
        params.splice(fileIndex, 1);
        params.unshift(fileField);
    }

    change(form) {
        this.setState({form}, () => {
            if (form.valid) this.onValidForm()
        });
    }

    onValidForm = () => {
        let config = this.convertToConfig(this.state.form);
        this.props.onValidConfig(config);
    };

    convertToConfig(form) {
        let config = {};
        config.params = this.convertParams(form.params);
        return config;
    }

    convertParams(params) {
        let result = [];
        params.forEach((p) => {
            if (Array.isArray(p.params)) {
                let configParam = this.createConfigParam(p, 0);
                result.push(configParam);
                configParam.params = this.convertParams(p.params);
            } else {
                p.value.forEach((v, i) => {
                    result.push(this.createConfigParam(p, i));
                });
            }
        });
        return result;
    }

    createConfigParam(p, valueIndex) {
        return {
            name: p.name,
            type: p.type,
            value: p.value[valueIndex]
        };
    }

    render() {
        const form = this.state.form;

        if (form === null) return <div>Loading...</div>;

        return (
            <div>
                <Form
                    form={form}
                    onChange={this.change}
                />
                <StatePropsViewer state={this.state} props={this.props}/>
            </div>
        );
    }
}

Configurator.propTypes = {
    service: PropTypes.string.isRequired,
    file: PropTypes.string.isRequired
};