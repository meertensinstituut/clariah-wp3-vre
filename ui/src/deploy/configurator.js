import React from "react";
import Switchboard from "../common/switchboard";
import Form from "./form/form";
import StatePropsViewer from "../common/state-props-viewer";

// TODO: hier gebleven!
// - ParamGroup wel dupliceren!
// - check how many elements and how many are allowed
// - allow multiple elements
// - remove elements
export default class Configurator extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            form: null,
            serviceParams: null
        };
        if (this.props.service !== undefined) {
            this.getServiceParams(this.props.service);
        }
        this.change = this.change.bind(this);

    }

    getServiceParams(service) {
        Switchboard.getParams(service).then((data) => {
            let form = this.createForm(data);
            this.setState({
                serviceParams: data,
                form: form
            });
        });
    }

    createForm(serviceParams) {
        let form = {params: []};
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
        this.addFormFields(formParam, parent);
        return formParam;
    }

    addFormFields(param, parent) {
        param.value = [""];
    }

    change(form) {
        this.setState({form});
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
                <StatePropsViewer state={this.state} props={this.props} />
            </div>
        );
    }
}