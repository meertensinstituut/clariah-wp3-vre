import React from "react";
import Switchboard from "../common/switchboard";
import Param from "./param";

// TODO: hier gebleven!
// - check how many elements and how many are allowed
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
        this.changeParam = this.changeParam.bind(this);
        this.addParam = this.addParam.bind(this);
    }

    changeParam(newFormParam) {
        let form = this.state.form;
        let index = this.findParamIndex(newFormParam.id, form.params);
        form.params[index] = newFormParam;
        this.setState({form});
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

    addFormFields(formParam, parent) {
        formParam.id = parent.params.length;
        formParam.parentId = parent.id;
    }

    addParam(param) {
        let copy = Object.assign({}, param);
        let params = this.findContainingParams(param, this.state.form);
        let index = this.findParamIndex(param.id, params);
        params.splice(index, 0, copy);
        copy.id = params.length;
        delete copy.value;
        this.setState({form: this.state.form});
    }

    findContainingParams(param, form) {
        if (isNaN(param.parentId)) {
            return form.params;
        }
        let parentIndex = this.findParamIndex(param.parentId, form.params);
        return form.params[parentIndex].params;
    }

    findParamIndex(id, params) {
        return params.findIndex(
            (p) => Number(p.id) === Number(id)
        );
    }

    render() {
        const form = this.state.form;

        if (form === null) return <div>Loading...</div>;

        return (
            <div>
                <form>
                    {form.params.map((param, i) => {
                        return <Param
                            key={i}
                            param={param}
                            onChange={this.changeParam}
                            onAdd={this.addParam}
                        />;
                    }, this)}
                </form>
                <div>
                    <p>state:</p>
                    <pre>{JSON.stringify(this.state, null, 2)}</pre>
                </div>
                <div>
                    <p>props:</p>
                    <pre>{JSON.stringify(this.props, null, 2)}</pre>
                </div>
            </div>
        );
    }
}