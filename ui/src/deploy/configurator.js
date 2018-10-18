import React from "react";
import DeployResource from "../common/deploy-resource";
import Form from "./form";
import PropTypes from 'prop-types';
import Dreamfactory from "../common/dreamfactory";
import ErrorMsg from "../common/error-msg";

/**
 * ServiceParams contains a json template from which a form is created.
 * Fields can be added according to min. and max. cardinality:
 * - Params with child params are duplicated
 * - Params without child params get an extra element in param.value[]
 */
export default class Configurator extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            form: null,
            serviceName: null,
            serviceParams: null
        };

        if (this.props.service !== undefined) {
            this.init();
        }

        this.change = this.change.bind(this);

    }

    async init() {
        const data = await DeployResource
            .getParams(this.props.service)
            .catch((e) => this.setState({error: e}));
        if(!data) return;
        let form = this.createForm(data);
        const objectData = await Dreamfactory
            .getObject(this.props.file)
            .catch((e) => this.setState({error: e}));
        this.setFile(form, objectData);
        const newState = {
            serviceName: data.name,
            serviceParams: data,
            form: form
        };
        this.setState(newState);
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
        if(!formParam.label) {
            formParam.label = formParam.name;
        }
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
            if (form.valid) {
                this.onValidForm();
            } else {
                this.props.onInvalid();
            }
        });
    }

    onValidForm = () => {
        let config = this.convertToConfig(this.state.form);
        this.props.onValid(this.state.serviceName, config);
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
        if (this.state.error)
            return <ErrorMsg title="Could not configure service" error={this.state.error}/>;

        const form = this.state.form;

        if (form === null) return <div>Loading...</div>;

        return (
            <div>
                <Form form={form} onChange={this.change}/>
            </div>
        );
    }
}

Configurator.propTypes = {
    service: PropTypes.number.isRequired,
    file: PropTypes.number.isRequired,
    onValid: PropTypes.func.isRequired,
    onInvalid: PropTypes.func.isRequired,
};