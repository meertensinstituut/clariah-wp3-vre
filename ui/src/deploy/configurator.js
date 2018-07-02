import React from "react";
import Switchboard from "../common/switchboard";
import Param from "./param";

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
        let index = this.findParamIndex(form, newFormParam);
        form.params[index] = newFormParam;
        this.setState({form});
    }

    findParamIndex(param, form) {
        return form.params.findIndex(
            (p) => Number(p.id) === Number(param.id)
        );
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
        formParam.params = [];
    }

    addParam(param) {
        console.log("addParam", param);
        let copy = Object.assign({}, param);
        if(param.parent === undefined) {
            let form = this.state.form;
            let index = this.findParamIndex(param, form);
            copy.id = form.params.length;
            form.params.splice(index, 0, copy);
            this.setState({form});
        } else {
            // TODO: hier gebleven!
        }
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