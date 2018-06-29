import React from "react";
import Switchboard from "../common/switchboard";
import Field from "./form/field";

export default class Configurator extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            serviceParams: null,
            label: "Do you breathe?",
            description: "Breathing is very important",
            value: "",
            placeholder: "Hello? Is it me you're looking for?"
        };
        if (this.props.service !== undefined) {
            this.getServiceParams(this.props.service);
        }
    }

    getServiceParams(service) {
        Switchboard.getParams(service).then((data) => {
            this.setState({
                serviceParams: data
            });
        });
    }

    handleValidationFormBasicText() {
        const length = this.state.value.length;
        if (length > 10) return 'success';
        if (length > 5) return 'warning';
        if (length > 0) return 'error';
        return null;
    }

    render() {
        if (this.state.serviceParams === null) {
            return <div>Loading...</div>
        }
        return (
            <div>
                <form>
                    {this.state.serviceParams.params.map(function (param, i) {
                        return <Field key={i} param={param}/>
                    }, this)}
                    {this.state.serviceParams.paramGroups.map(function (paramGroup, i) {
                        return <div key={i}>
                            {paramGroup.params.map(function (param, k) {
                                return <Field key={k} param={param}/>
                            }, this)}
                        </div>
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