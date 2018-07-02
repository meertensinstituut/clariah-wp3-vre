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
        this.onChangeParam = this.onChangeParam.bind(this);
    }

    onChangeParam(newFormParam) {
        let form = this.state.form;
        if (newFormParam.parentId === undefined) {
            let index = form.params.findIndex(
                (param) => Number(param.id) === Number(newFormParam.id)
            );
            form[index] = newFormParam;
        }
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
                            onChange={this.onChangeParam}
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